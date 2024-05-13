package org.chaz.membershipsite.settings.controller;

import org.chaz.membershipsite.settings.repository.StateRepository;
import org.membershipcommons.dto.StateDto;
import org.membershipcommons.entity.State;
import org.membershipcommons.entity.settings.Country;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/settings")
public class StateRestController {

    @Autowired
    private StateRepository stateRepository;

    @GetMapping("/list-states-by-country/{id}")
    public List<StateDto> listStatesByCountry(@PathVariable("id") Integer countryId) {
        List<State> stateList = stateRepository.findByCountryOrderByNameAsc(new Country(countryId));
        List<StateDto> result = new ArrayList<>();

        for (State state : stateList) {
            result.add(new StateDto(state.getId(), state.getName()));
        }

        return result;
    }
}
