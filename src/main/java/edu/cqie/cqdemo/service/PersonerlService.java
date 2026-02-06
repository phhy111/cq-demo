package edu.cqie.cqdemo.service;

import edu.cqie.cqdemo.entity.Users;

public interface PersonerlService {
    public Users getUser(Long id);
    public Users getUserByUsername(String username);
}
