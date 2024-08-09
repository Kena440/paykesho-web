package org.paykesho.clientportal.security;

import org.commons.entity.client.Client;
import org.paykesho.clientportal.clients.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class ClientUserDetailsService implements UserDetailsService {

    @Autowired
    private ClientRepository clientRepository;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        Client client = clientRepository.findByEmail(s);

        if (client == null) throw new UsernameNotFoundException("No assessor found");

        return new ClientUserDetails(client);

    }
}
