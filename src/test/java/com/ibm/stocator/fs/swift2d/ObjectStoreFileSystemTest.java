package com.ibm.stocator.fs.swift2d;

import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.FSDataInputStream;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.ibm.stocator.fs.ObjectStoreFileSystem;
import com.ibm.stocator.fs.common.Constants;

public class ObjectStoreFileSystemTest extends SwiftBaseTest {

  private ObjectStoreFileSystem mMockObjectStoreFileSystem;
  private String hostName = "swift2d://out1003.lvm";
  protected byte[] data = SwiftTestUtils.generateDataset(getBlockSize() * 2, 0, 255);

  @Before
  public final void before() {
    mMockObjectStoreFileSystem = PowerMockito.mock(ObjectStoreFileSystem.class);
    Whitebox.setInternalState(mMockObjectStoreFileSystem, "hostNameScheme", hostName);
  }

  @Test
  public void getObjectNameTest() throws Exception {
    Path input = new Path("swift2d://out1003.lvm/a/b/c/m.data/_temporary/"
            + "0/_temporary/attempt_201603141928_0000_m_000099_102/part-00099");
    String result = Whitebox.invokeMethod(mMockObjectStoreFileSystem, "getObjectName", input,
            Constants.HADOOP_TEMPORARY, true);
    Assert.assertEquals("/a/b/c/m.data/part-00099-attempt_201603141928_0000_m_000099_102", result);

    input = new Path("swift2d://out1003.lvm/a/b/m.data/_temporary/"
            + "0/_temporary/attempt_201603141928_0000_m_000099_102/part-00099");
    result = Whitebox.invokeMethod(mMockObjectStoreFileSystem, "getObjectName", input,
            Constants.HADOOP_TEMPORARY, true);
    Assert.assertEquals("/a/b/m.data/part-00099-attempt_201603141928_0000_m_000099_102", result);

    input = new Path("swift2d://out1003.lvm/m.data/_temporary/"
            + "0/_temporary/attempt_201603141928_0000_m_000099_102/part-00099");
    result = Whitebox.invokeMethod(mMockObjectStoreFileSystem, "getObjectName", input,
            Constants.HADOOP_TEMPORARY, true);
    Assert.assertEquals("/m.data/part-00099-attempt_201603141928_0000_m_000099_102", result);

  }

  @Test(expected = IOException.class)
  public void getObjectWrongNameTest() throws Exception {
    Path input = new Path("swift2d://out1003.lvm_temporary/"
            + "0/_temporary/attempt_201603141928_0000_m_000099_102/part-00099");
    Whitebox.invokeMethod(mMockObjectStoreFileSystem, "getObjectName", input,
            Constants.HADOOP_TEMPORARY, true);

    input = new Path("swift2d://out1003.lvm/temporary/"
            + "0/_temporary/attempt_201603141928_0000_m_000099_102/part-00099");
    Whitebox.invokeMethod(mMockObjectStoreFileSystem, "getObjectName", input,
            Constants.HADOOP_TEMPORARY, true);
  }

  @Test
  public void getSchemeTest() throws Exception {
    Assume.assumeNotNull(getFs());
    Assert.assertEquals(Constants.SWIFT2D, getFs().getScheme());
  }

  @Test
  public void existsTest() throws Exception {
    Assume.assumeNotNull(getFs());
    Path testFile = new Path(getBaseURI() + "/testFile");
    getFs().delete(testFile, false);
    Assert.assertFalse(getFs().exists(testFile));

    createFile(testFile, data);
    Assert.assertTrue(getFs().exists(testFile));

    Path input = new Path(getBaseURI() + "/a/b/c/m.data/_temporary/"
            + "0/_temporary/attempt_201603141928_0000_m_000099_102/part-00099");
    Whitebox.setInternalState(mMockObjectStoreFileSystem, "hostNameScheme", getBaseURI());
    String result = Whitebox.invokeMethod(mMockObjectStoreFileSystem, "getObjectName", input,
            Constants.HADOOP_TEMPORARY, true);
    Path modifiedInput = new Path(getBaseURI() + result);
    getFs().delete(input, false);
    Assert.assertFalse(getFs().exists(input));

    createFile(input, data);
    Assert.assertFalse(getFs().exists(input));
    Assert.assertTrue(getFs().exists(modifiedInput));
  }

  @Test
  public void listLocatedStatusTest() throws Exception {
    Assume.assumeNotNull(getFs());
    int iterNum = 3;
    Path[] testFile0 = new Path[iterNum];
    String fileName = getBaseURI() + "/testFile";
    for (int i = 0; i < iterNum; i++) {
      testFile0[i] = new Path(fileName + "0" + i);
      createFile(testFile0[i], data);
    }
    Path[] testFile1 = new Path[iterNum * 2];
    for (int i = 0; i < iterNum * 2; i++) {
      testFile1[i] = new Path(fileName + "1" + i);
      createFile(testFile1[i], data);
    }

    int count = 0;
    RemoteIterator<LocatedFileStatus> stats = getFs().listLocatedStatus(new Path(fileName));
    while (stats.hasNext()) {
      LocatedFileStatus stat = stats.next();
      Assert.assertTrue(stat.getPath().getName().startsWith("testFile"));
      count++;
    }
    Assert.assertEquals(iterNum * 3, count);

    count = 0;
    stats = getFs().listLocatedStatus(new Path(fileName + "0"));
    while (stats.hasNext()) {
      LocatedFileStatus stat = stats.next();
      Assert.assertTrue(stat.getPath().getName().startsWith("testFile0"));
      count++;
    }
    Assert.assertEquals(iterNum, count);
  }

  @Test
  public void openCreateTest() throws Exception {
    Assume.assumeNotNull(getFs());
    Path testFile = new Path(getBaseURI() + "/testFile");
    createFile(testFile, data);
    FSDataInputStream inputStream = getFs().open(testFile);
    long bufferSize = Whitebox.getInternalState(inputStream.getWrappedStream(), "bufferSize");
    Assert.assertEquals(65536, bufferSize);

    Path path = Whitebox.getInternalState(inputStream.getWrappedStream(), "path");
    Assert.assertEquals(testFile.getName(), path.getName());
  }

  @Test
  public void deleteTest() throws Exception {
    Assume.assumeNotNull(getFs());

    Path testFile = new Path(getBaseURI() + "/testFile");
    createFile(testFile, data);
    Path input = new Path(getBaseURI() + "/a/b/c/m.data/_temporary/"
            + "0/_temporary/attempt_201603141928_0000_m_000099_102/part-00099");
    Whitebox.setInternalState(mMockObjectStoreFileSystem, "hostNameScheme", getBaseURI());
    String result = Whitebox.invokeMethod(mMockObjectStoreFileSystem, "getObjectName", input,
            Constants.HADOOP_TEMPORARY, true);
    Path modifiedInput = new Path(getBaseURI() + result);
    createFile(input, data);
    Assert.assertTrue(getFs().exists(modifiedInput));
    Assert.assertTrue(getFs().exists(testFile));

    getFs().delete(testFile, false);
    Assert.assertFalse(getFs().exists(testFile));

    getFs().delete(input, false);
    Assert.assertFalse(getFs().exists(modifiedInput));
  }

  @Test
  public void listStatusTest() throws Exception {
    Assume.assumeNotNull(getFs());
    Path testFile = new Path(getBaseURI() + "/testFile");
    getFs().delete(testFile, false);
    FileStatus[] stats = getFs().listStatus(testFile);
    Assert.assertTrue(0 == stats.length);

    createFile(testFile, data);
    Assert.assertTrue(getFs().exists(testFile));
    stats = getFs().listStatus(testFile);
    Assert.assertTrue(1 == stats.length);

    FileStatus stat = stats[0];
    Assert.assertEquals("testFile", stat.getPath().getName());
    Assert.assertFalse(stat.isDirectory());
    Assert.assertTrue(stat.isFile());
    Assert.assertEquals(data.length, stat.getLen());

    getFs().delete(testFile, false);
    stats = getFs().listStatus(testFile);
    Assert.assertTrue(0 == stats.length);
  }
}
