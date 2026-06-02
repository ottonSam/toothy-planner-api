package br.com.ottonsam.toothy_planner_api.user.usecases;

public final class ProfileImagePayload {

    private final byte[] content;
    private final String contentType;
    private final String extension;

    public ProfileImagePayload(byte[] content, String contentType, String extension) {
        this.content = content.clone();
        this.contentType = contentType;
        this.extension = extension;
    }

    public byte[] content() {
        return content.clone();
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }
}
