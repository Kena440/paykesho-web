package org.paykesho.backoffice;

import org.commons.entity.Role;
import org.commons.entity.User;
import org.commons.enums.GenderType;
import org.paykesho.backoffice.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.Rollback;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = false)
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;

    //@Test
    public void testCreateUserWithOneRole() {
        Role roleAdmin = entityManager.find(Role.class, 1);
        User user = new User("mwamba.kaenga@chaz.org.zm", "$2a$10$KdMDApyzJeXP6XVpXtutn.BQ42VvFhkXHm67F2OZkbuuuf1jFQbKm", "Mwamba", "Kaenga");
        user.addRole(roleAdmin);
        user.setEnabled(true);
        User savedUser = userRepository.save(user);
        assertThat(savedUser.getId()).isGreaterThan(0);
    }


    //@Test
    public void testCreateUserWithTwoRole() {
        Role roleAdmin = entityManager.find(Role.class, 1);
        User user = new User("mekaenga@yahoo.com", "$2a$10$KdMDApyzJeXP6XVpXtutn.BQ42VvFhkXHm67F2OZkbuuuf1jFQbKm", "Elias", "Kaenga");
        Role roleEditor = new Role(3);
        Role roleAssistant = new Role(5);
        user.addRole(roleEditor);
        user.addRole(roleAssistant);
        User savedUser = userRepository.save(user);
        assertThat(savedUser.getId()).isGreaterThan(0);
    }

    //@Test
    public void listAllUsers() {
        Iterable<User> allUsers = userRepository.findAll();
        allUsers.forEach(user -> System.out.println(user));
    }

    //@Test
    public void getUserById() {
        User user = userRepository.findById(1).get();
        System.out.println(user);
        assertThat(user).isNotNull();
    }

    //@Test
    public void updateUserDetails() {
        User user = userRepository.findById(1).get();
        user.setEnabled(true);
        user.setGender(GenderType.MALE);
        User updatedUser = userRepository.save(user);
        assertThat(updatedUser).isNotNull();
    }

    //@Test
    public void updateUserRolesDetails() {
        User user = userRepository.findById(2).get();
        Role roleEditor = new Role(3);
        Role roleSales = new Role(2);

        user.getRoles().remove(roleEditor);
        user.addRole(roleSales);
        User updatedUser = userRepository.save(user);
        assertThat(updatedUser).isNotNull();
    }

    //@Test
    public void deleteUserById() {
        Integer userId = 2;
        userRepository.deleteById(userId);
    }

    //@Test
    public void testGetUserByEmail() {
        String email = "kaengamwamba@gmail.com";
        User user = userRepository.getUserByEmail(email);

        assertThat(user.getId()).isPositive();
    }

    //@Test
    public void testCountById() {
        Integer id = 8;
        Long countById = userRepository.countById(id);

        assertThat(countById).isNotNull().isPositive();
    }

    //@Test
    public void testDisableUser() {
        Integer id = 10;
        userRepository.updateEnableStatus(id, false);
    }

    //@Test
    public void testEnableUser() {
        Integer id = 10;
        userRepository.updateEnableStatus(id, true);
    }

    //@Test
    public void testListFirstPage() {
        int pageNumber = 0;
        int pageSize = 2;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<User> page = userRepository.findAll(pageable);
        List<User> listUsers = page.getContent();

        listUsers.forEach(System.out::println);
    }

    //@Test
    public void testSearchUsers() {
        String keyword = "kaenga";

        int pageNumber = 0;
        int pageSize = 2;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<User> page = userRepository.findAll(keyword, pageable);
        List<User> listUsers = page.getContent();

        listUsers.forEach(System.out::println);

        assertThat(listUsers.size()).isPositive();
    }
}
