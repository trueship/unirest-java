/*
The MIT License

Copyright (c) 2013 Mashape (http://mashape.com)

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.mashape.unirest.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

import com.mashape.unirest.http.utils.ResponseUtils;
import org.apache.log4j.Logger;

public class HttpResponse<T> {

	private int code;
	private Headers headers = new Headers();
	private InputStream rawBody;
	private T body;

	@SuppressWarnings("unchecked")
	public HttpResponse(org.apache.http.HttpResponse response, Class<T> responseClass) {
		HttpEntity responseEntity = response.getEntity();

		Header[] allHeaders = response.getAllHeaders();
		for(Header header : allHeaders) {
			String headerName = header.getName().toLowerCase();
			List<String> list = headers.get(headerName);
			if (list == null) list = new ArrayList<String>();
			list.add(header.getValue());
			headers.put(headerName, list);
		}
		this.code = response.getStatusLine().getStatusCode();

		if (responseEntity != null) {
			String charset = "UTF-8";
			
			Header contentType = responseEntity.getContentType();
			if (contentType != null) {
				String responseCharset = ResponseUtils.getCharsetFromContentType(contentType.getValue());
				if (responseCharset != null && !responseCharset.trim().equals("")) {
					charset = responseCharset;
				}
			}
		
			try {
				byte[] rawBody;
				try {
					InputStream responseInputStream = responseEntity.getContent();
					if (ResponseUtils.isGzipped(responseEntity.getContentEncoding())) {
						responseInputStream = new GZIPInputStream(responseEntity.getContent());
					}
					rawBody = ResponseUtils.getBytes(responseInputStream);
				} catch (IOException e2) {
					throw new RuntimeException(e2);
				}
				InputStream inputStream = new ByteArrayInputStream(rawBody);
				this.rawBody = inputStream;

				if (JsonNode.class.equals(responseClass)) {
					String jsonString = new String(rawBody, charset).trim();
                    try {
    					this.body = (T) new JsonNode(jsonString);
                    } catch (RuntimeException e) {
                        Logger.getLogger(HttpResponse.class).error("Unable to parse response as JSON:\n" + jsonString);
                        throw e;
                    }
				} else if (String.class.equals(responseClass)) {
					this.body = (T) new String(rawBody, charset);
				} else if (InputStream.class.equals(responseClass)) {
					this.body = (T) this.rawBody;
				} else {
					throw new Exception("Unknown result type. Only String, JsonNode and InputStream are supported.");
				}
            } catch (Exception e) {
				throw new RuntimeException("Unable to parse response.", e);
			}
		}
	}

	public int getCode() {
		return code;
	}

	public Headers getHeaders() {
		return headers;
	}

	public InputStream getRawBody() {
		return rawBody;
	}

	public T getBody() {
		return body;
	}

}
