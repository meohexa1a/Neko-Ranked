package neko.testing.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datacs")
public class DatacController  {

    @Autowired
    private DatacRepo datacRepository;

    @PostMapping
    public Datac createDatac(@RequestBody Datac datac) {
        return datacRepository.save(datac);
    }

    
}