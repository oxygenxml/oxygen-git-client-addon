package com.oxygenxml.git.auth;

import java.io.File;

import org.eclipse.jgit.transport.sshd.DefaultProxyDataFactory;

import junit.framework.TestCase;

public class ResolvingProxyDataFactoryTest extends TestCase {

  /**
   * <p><b>Description:</b> When upgrading the Apache MINA SSHD version,
   * check if the "socks" URI schema has been changed to "socket".
   * It should be changed in version 5.5. Also check any other changes,
   * and keep our patches, if needed.</p>
   * <p><b>Bug ID:</b> EXM-43640</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testApacheMINAVersionAndPatches() throws Exception {
    String name = new File(DefaultProxyDataFactory.class.getProtectionDomain()
        .getCodeSource().getLocation().toURI()).getName();
    assertEquals("The Apache MINA SSHD library has been updated."
        + " Check " + ResolvingProxyDataFactory.class.getName() + " for "
        + "updating the patches, if needed.",
        "org.eclipse.jgit.ssh.apache-5.4.2.201908231537-r.jar",
        name);
  }
  
}
