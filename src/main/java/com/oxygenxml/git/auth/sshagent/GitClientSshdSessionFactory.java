package com.oxygenxml.git.auth.sshagent;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.transport.sshd.ProxyDataFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.agent.Connector;
import org.eclipse.jgit.transport.sshd.agent.ConnectorFactory;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.utils.PlatformDetectionUtil;

/**
 * Custom implementation for SshSessionFactory, used to manage the SSH Agent support.
 * A SshSessionFactory that uses Apache MINA sshd. Classes from ApacheMINA sshd are kept private to avoid API evolution problems when Apache MINAsshd interfaces change.
 * 
 * @author alex_smarandache
 *
 */
public class GitClientSshdSessionFactory extends SshdSessionFactory {

  /**
   * The options manager.
   */
  private static final OptionsManager OPTIONS_MANAGER = OptionsManager.getInstance();

  /**
   * Constructor.
   * 
   * @param proxyFactory The proxy database.
   */
  public GitClientSshdSessionFactory(@Nullable final ProxyDataFactory proxyFactory) {
    super(null, proxyFactory);
  }

  @Override
  protected ConnectorFactory getConnectorFactory() {
    final ConnectorFactory factory = super.getConnectorFactory();
    return factory != null ? new WrappedSshAgentConnectorFactory(factory) : null;
  }


  /**
   * Wrapper over a ConnectorFactory.
   * 
   * @author alex_smarandache
   *
   */
  private static class WrappedSshAgentConnectorFactory implements ConnectorFactory {
    
    /**
     * The delegated connector factory.
     */
    private final ConnectorFactory delegate;

    /**
     * Constructor.
     * 
     * @param realFactory The delegated connector factory.
     */
    WrappedSshAgentConnectorFactory(@NonNull ConnectorFactory realFactory) {
      delegate = realFactory;
    }

    /**
     * This method use the default preferred SSH agent or WIN32_OPENSSH for Windows or the default SSH agent for Unix.
     * 
     * @param identityAgent The identity of the agent.
     * @param homeDir       The home directory.
     */
    @Override
    public Connector create(final String identityAgent, final File homeDir)
        throws IOException {

      if(!OPTIONS_MANAGER.getUseSshAgent()) {
        return null;
      }

      String agentConnection = identityAgent;
      if (identityAgent == null || identityAgent.isEmpty()) {
        SSHAgent sshAgent = SSHAgent.getByName(OptionsManager.getInstance().getDefaultSshAgent());
        if(PlatformDetectionUtil.isWin()) {
          if(!SSHAgent.isForWin(sshAgent)) {
            sshAgent = SSHAgent.WIN_WIN32_OPENSSH;
          }
          final SSHAgent finalSSHAg = sshAgent;
          if (getSupportedConnectors().stream().anyMatch(d -> finalSSHAg.getIdentityAgent().equals(d.getIdentityAgent()))) {
            agentConnection = sshAgent.getIdentityAgent();
          }
        } else {
          final Collection<ConnectorDescriptor> connectors = getSupportedConnectors();
          if(!connectors.isEmpty()) {
            agentConnection = connectors.iterator().next().getIdentityAgent();
          }
        }
      }

      return delegate.create(agentConnection, homeDir);
    }

    @Override
    public boolean isSupported() {
      return delegate.isSupported();
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
    public Collection<ConnectorDescriptor> getSupportedConnectors() {
      return delegate.getSupportedConnectors();
    }

    @Override
    public ConnectorDescriptor getDefaultConnector() {
      return delegate.getDefaultConnector();
    }
  }

}
