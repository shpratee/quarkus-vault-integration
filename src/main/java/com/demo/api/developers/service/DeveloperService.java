package com.demo.api.developers.service;

import com.demo.api.developers.model.Developer;
import com.demo.api.developers.model.Skill;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class DeveloperService {

    private static final Map<String, Developer> developers = new HashMap<>();

    public Developer[] getDevelopers() {

        Set keys = developers.keySet();
        Developer[] devArray = new Developer[keys.size()];

        List<Developer> devList = new ArrayList<>();

        Iterator keysIter = keys.iterator();
        while(keysIter.hasNext()){
            devList.add(developers.get(keysIter.next()));
        }

        return devList.toArray(new Developer[devList.size()]);
    }

    public Developer getDeveloper(String id) {
        return developers.get(id);
    }

    public void addDeveloper(Developer developer) {
        developers.put(developer.getId(), developer);
    }
}
