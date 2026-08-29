package com.ordreDeMission.ordreDeMissison.config;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.repository.EmployeeRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public CustomUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String matricule) throws UsernameNotFoundException {
        Employee emp = employeeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new UsernameNotFoundException("Matricule invalide : " + matricule));
        return new User(emp.getMatricule(), emp.getMotDePasseHash(),
                List.of(new SimpleGrantedAuthority(emp.getRole())));
    }
}
