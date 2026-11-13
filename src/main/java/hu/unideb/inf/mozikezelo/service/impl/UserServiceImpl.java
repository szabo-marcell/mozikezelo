package hu.unideb.inf.mozikezelo.service.impl;

import hu.unideb.inf.mozikezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.mozikezelo.service.UserService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final FelhasznaloRepository repo;

    public UserServiceImpl(FelhasznaloRepository repo) {
        this.repo = repo;
    }


    @Override
    public UserDetailsService getUserDetailsService() {
        return username -> repo.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
