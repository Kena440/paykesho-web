package org.chaz.backoffice.settings.controller;

import org.chaz.backoffice.settings.repository.CountryRepository;
import org.commons.entity.settings.Country;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/countries")
public class CountryRestController {
    @Autowired
    private CountryRepository countryRepository;

    @GetMapping("/list")
    public List<Country> countryList() {
        return countryRepository.findAllByOrderByNameDesc();
    }

    @PostMapping("/save")
    public String save(@RequestBody Country country) {
        Country savedCountry = countryRepository.save(country);
        return String.valueOf(savedCountry.getId());
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable("id") Integer id) {
        countryRepository.deleteById(id);
    }
}
