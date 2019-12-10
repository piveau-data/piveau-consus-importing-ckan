package io.piveau.importing.ckan.response;

public abstract class HttpResult<T> {

    protected final T result;

    protected HttpResult(T result) {
        this.result = result;
    }

    protected abstract T getContent();

}
