package uk.ac.cam.cl.charlie.filewalker;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Created by Louis-Pascal on 23/02/2017
 */
public class FileWalkerTestSuite {

	static String CWD; 
	static String ROOT;
	static String TESTPATH = "\\src\\main\\resources\\filewalkertest";
	static FileWalker test;

	@ClassRule
	public final static TemporaryFolder root = new TemporaryFolder();
	
	@BeforeClass
	public static void setUp() {
		System.out.println("Setting Up");
		CWD = System.getProperty("user.dir");
		
		try {
			ROOT = root.getRoot().toString();
			System.out.println(ROOT);
			System.out.println(CWD + TESTPATH);
			FileUtils.copyDirectory(new File(CWD + TESTPATH), root.getRoot());
		} catch (IOException e) {
			System.err.println("Could not initialise test folder structure");
			e.printStackTrace();
		} 
		test = new BasicFileWalker(Paths.get(ROOT+"\\root1\\"));
	}
	
	@Test
	public void removeNonExistentRoot() {
		test.removeRootDirectory(Paths.get(""));
	}
	
	@Test(expected = NullPointerException.class)
	public void addNullRoot() {
		test.addRootDirectory(null);
	}
	
	@Test
	public void walkRoot() {
		test.addRootDirectory(Paths.get(ROOT + "\\root2\\"));
		test.addRootDirectory(Paths.get(ROOT+"\\root3\\"));
		test.startWalkingTree();
	}
	
	@Test
	public void duplicateAdd() {
		test.addRootDirectory(Paths.get(ROOT + "\\root1\\"));
		test.removeRootDirectory(Paths.get(ROOT + "\\root1\\"));
		for(Path p: test.getRootDirectories()){
			assertFalse(p.toString().equals(ROOT + "\\root1\\"));
		}
		test.addRootDirectory(Paths.get(ROOT + "\\root1\\"));
	}
	
	@AfterClass
	public static void reset() {
		test.closeListener();
	}

}
