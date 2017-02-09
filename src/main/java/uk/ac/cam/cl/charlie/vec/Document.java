package uk.ac.cam.cl.charlie.vec;

public class Document {
	
	private String content;
	private String filename;

	private boolean hasBeenVectorised = false;
	
	public Document(String filename, String content) {
		this.content = content;
		this.filename = filename;
	}

	public String getName() {
		return filename;
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

}
