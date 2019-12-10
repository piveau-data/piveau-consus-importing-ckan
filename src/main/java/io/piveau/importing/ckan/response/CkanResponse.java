package io.piveau.importing.ckan.response;

import io.vertx.core.json.JsonObject;

public class CkanResponse extends HttpResponse<JsonObject> {

	public CkanResponse(JsonObject content) {
		super(content);
	}

	@Override
	public boolean isError() {
		return content != null && !content.getBoolean("success");
	}

	@Override
	public boolean isSuccess() {
		return content != null && content.getBoolean("success");
	}
	
	public String getHelp() {
		return content == null ? "" : content.getString("help");
	}

	@Override
	public CkanError getError() {
		return !isError() ? null : new CkanError(content.getJsonObject("error"));
	}

	@Override
	public CkanArrayResult getArrayResult() {
		return isError() ? null : new CkanArrayResult(content.getJsonArray("result"));
	}

	@Override
	public CkanObjectResult getObjectResult() {
		return isError() ? null : new CkanObjectResult(content.getJsonObject("result"));
	}

}
