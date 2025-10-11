package org.paykesho.backoffice.settings.controller;

import org.commons.entity.settings.Country;
import org.paykesho.backoffice.settings.repository.CountryRepository;
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

    @DeleteMapping("/delete/{uuid}")
    public void delete(@PathVariable("uuid") String uuid) {
        Country country = countryRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Country not found with UUID: " + uuid));
        countryRepository.deleteById(country.getId());
    }
}
