package uk.ac.cam.cl.charlie.vec;

import java.nio.file.Path;

public class Document {
	
	private String content;
	private Path path;

	private boolean hasBeenVectorised = false;
	
	public Document(Path p, String content) {
		this.content = content;
		this.path = p;
	}

	public String getContent() {
		return content;
	}

	public void setVectorisationStatus(boolean status) {
		hasBeenVectorised = status;
	}

	public boolean hasBeenVectorised() {
	    return hasBeenVectorised;
    }

	public Path getPath() {
		return path;
	}
}
