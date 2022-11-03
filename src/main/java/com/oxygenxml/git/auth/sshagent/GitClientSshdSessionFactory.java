package com.oxygenxml.git.auth.sshagent;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.transport.sshd.ProxyDataFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.agent.Connector;
import org.eclipse.jgit.transport.sshd.agent.ConnectorFactory;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.utils.PlatformDetectionUtil;

public class GitClientSshdSessionFactory extends SshdSessionFactory {

  private static final OptionsManager OPTIONS_MANAGER = OptionsManager.getInstance();

  public GitClientSshdSessionFactory(ProxyDataFactory proxyFactory) {
    super(null, proxyFactory);
  }

  @Override
  protected ConnectorFactory getConnectorFactory() {
    final ConnectorFactory factory = super.getConnectorFactory();
    return factory != null ? new WrappedSshAgentConnectorFactory(factory) : null;
  }


  private static class WrappedSshAgentConnectorFactory implements ConnectorFactory {

    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private final ConnectorFactory delegate;

    WrappedSshAgentConnectorFactory(@NonNull ConnectorFactory realFactory) {
      delegate = realFactory;
    }

    @Override
    public Connector create(String identityAgent, File homeDir)
        throws IOException {

      if(!OPTIONS_MANAGER.getUseSshAgent()) {
        return null;
      }

      String agentConnection = identityAgent;
      if (identityAgent == null || identityAgent.isEmpty()) {
        SSHAgent sshAgent = SSHAgent.getByName(OptionsManager.getInstance().getDefaultSshAgent());
        if(PlatformDetectionUtil.isWin()) {
          if(!SSHAgent.isForWin(sshAgent)) {
            sshAgent = SSHAgent.WIN_PAGEAGENT;
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
