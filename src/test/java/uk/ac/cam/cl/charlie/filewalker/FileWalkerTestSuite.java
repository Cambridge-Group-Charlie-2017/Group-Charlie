package uk.ac.cam.cl.charlie.filewalker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import uk.ac.cam.cl.charlie.math.Vector;

/**
 * Created by Louis-Pascal on 23/02/2017
 */
public class FileWalkerTestSuite {

	static String CWD; 
	static String ROOT;
	static String TESTPATH = "\\src\\main\\resources\\filewalkertest";
	static FileWalker test;
	static FileDB db;

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
		db = FileDB.getInstance();
		test = new BasicFileWalker();
		test.addRootDirectory(Paths.get(ROOT+"\\root1\\"));
	}

	@AfterClass
	public static void reset() {
		test.closeListener();
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
	public void addRoot() {
		test.addRootDirectory(Paths.get(ROOT + "\\root2\\"));
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
	
	@Test
	public void foundAllFiles() {
		test.addRootDirectory(Paths.get(ROOT + "\\root2\\"));
		test.removeRootDirectory(Paths.get(ROOT + "\\root3\\"));
		test.startWalkingTree();
		Set<Path> prioFiles = db.getPriorityFiles();
		for(int i = 111; i <= 150; ++i) {
			assertTrue(prioFiles.contains(Paths.get(ROOT + "\\root1\\subfolder\\" + i + ".txt")));
		}
		for(int i = 181; i<=200; ++i) {
			assertTrue(prioFiles.contains(Paths.get(ROOT + "\\root2\\" + i + ".txt")));
		}
		for(int i = 151; i<=180; ++i) {
			assertTrue(prioFiles.contains(Paths.get(ROOT + "\\root2\\subfolder\\" + i + ".txt")));
		}
	}
	
	@Test
	public void deleteFiles() throws IOException {
		test.addRootDirectory(Paths.get(ROOT + "\\root2\\"));
		test.removeRootDirectory(Paths.get(ROOT + "\\root3\\"));
		FileUtils.deleteDirectory(new File(ROOT + "\\root1\\subfolder\\"));
		test.startWalkingTree();
		Set<Path> prioFiles = db.getPriorityFiles();
		for(int i = 111; i <= 150; ++i) {
			assertFalse(prioFiles.contains(Paths.get(ROOT + "\\root1\\subfolder\\" + i + ".txt")));
		}
		FileUtils.copyDirectory(new File(CWD + TESTPATH + "\\root1\\"), new File(ROOT+"\\root1\\"));
	}
	
	@Test
	public void addAndChangeFiles() throws IOException {
		test.addRootDirectory(Paths.get(ROOT + "\\root2\\"));
		test.removeRootDirectory(Paths.get(ROOT + "\\root3\\"));
		FileUtils.copyDirectory(new File(ROOT + "\\root3\\"), new File(ROOT + "\\root1\\subfolder1\\"));
		test.startWalkingTree();
		Set<Path> prioFiles = db.getPriorityFiles();
		for(int i = 100; i <= 110; ++i) {
			assertTrue(prioFiles.contains(Paths.get(ROOT + "\\root1\\subfolder1\\" + i + ".txt")));
		}
		Vector originalVec = db.getVector(Paths.get(ROOT + "\\root1\\subfolder1\\100.txt"));
		//appending file 101.txt to end of 100.txt, expecting the vector for text 100.txt to change
		File testfile = new File(ROOT + "\\root1\\subfolder1\\100.txt");
		File appendfile = new File(ROOT + "\\root1\\subfolder1\\101.txt");
		FileUtils.writeStringToFile(testfile, FileUtils.readFileToString(appendfile, (Charset) null), (Charset) null, true);
		assertTrue(originalVec != db.getVector(Paths.get(ROOT + "\\root1\\subfolder1\\100.txt")));
		FileUtils.deleteDirectory(new File(ROOT+"\\root1\\subfolder1"));
	}

}
