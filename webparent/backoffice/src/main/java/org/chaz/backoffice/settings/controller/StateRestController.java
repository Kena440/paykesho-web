package org.chaz.backoffice.settings.controller;

import org.chaz.backoffice.settings.repository.StateRepository;
import org.membershipcommons.dto.StateDto;
import org.membershipcommons.entity.State;
import org.membershipcommons.entity.settings.Country;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/states")
public class StateRestController {

    @Autowired
    private StateRepository stateRepository;

    @GetMapping("/list-by-country/{id}")
    public List<StateDto> listByCountry(@PathVariable("id") Integer countryId) {
        List<State> stateList = stateRepository.findByCountryOrderByNameAsc(new Country(countryId));
        List<StateDto> result = new ArrayList<>();

        for (State state : stateList) {
            result.add(new StateDto(state.getId(), state.getName()));
        }

        return result;
    }

    @PostMapping("/save")
    public String save(@RequestBody State state) {
        State savedState = stateRepository.save(state);
        return String.valueOf(savedState.getId());
    }

    @GetMapping("/delete/{id}")
    public void delete(@PathVariable("id") Integer id) {
        stateRepository.deleteById(id);
    }

}
