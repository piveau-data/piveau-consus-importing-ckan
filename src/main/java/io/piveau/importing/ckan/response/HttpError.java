package io.piveau.importing.ckan.response;

public abstract class HttpError<T> {
    protected final T error;

    protected HttpError(T error) {
        this.error = error;
    }

    protected abstract String getType();

    protected abstract String getMessage();

}
