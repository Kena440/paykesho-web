package org.paykesho.merchantportal.settings.controller;

import org.commons.dto.StateDto;
import org.commons.entity.State;
import org.commons.entity.settings.Country;
import org.paykesho.merchantportal.settings.repository.StateRepository;
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

    @GetMapping("/list-states-by-country/{uuid}")
    public List<StateDto> listStatesByCountry(@PathVariable("uuid") String countryUuid) {
        List<State> stateList = stateRepository.findByCountryUuid(countryUuid);
        List<StateDto> result = new ArrayList<>();

        for (State state : stateList) {
            result.add(new StateDto(state.getId(), state.getName()));
        }

        return result;
    }
}
