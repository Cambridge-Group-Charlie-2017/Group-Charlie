package uk.ac.cam.cl.charlie.vec;

public class Document {
	
	private String content;
	private String filename;
	
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
}
