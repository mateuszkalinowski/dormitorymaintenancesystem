package pl.dormitorymaintenancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import pl.dormitorymaintenancesystem.enumTranslation.UserStatusTranslation;
import pl.dormitorymaintenancesystem.enums.UserRoleEnum;
import pl.dormitorymaintenancesystem.enums.UserStatusEnum;
import pl.dormitorymaintenancesystem.model.Category;
import pl.dormitorymaintenancesystem.model.UserRole;
import pl.dormitorymaintenancesystem.model.users.Worker;
import pl.dormitorymaintenancesystem.repositories.CategoryRepository;
import pl.dormitorymaintenancesystem.repositories.WorkerRepository;
import pl.dormitorymaintenancesystem.utils.Page;
import pl.dormitorymaintenancesystem.utils.dataInput.NewWorkerData;
import pl.dormitorymaintenancesystem.utils.dataInput.WorkerInfo;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Transactional
public class WorkerService {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ResponseEntity updateWorkerInfo(WorkerInfo workerInfo) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentEmail = authentication.getName();
            Worker worker = workerRepository.findByEmail(currentEmail);
            if (worker == null)
                return ResponseEntity.badRequest().build();

            if(workerInfo.getContactEmail().length()<255 && workerInfo.getPhone().length()<255) {
                worker.setContactEmail(workerInfo.getContactEmail());
                worker.setPhoneNumber(workerInfo.getPhone());
                workerRepository.save(worker);
                return ResponseEntity.ok().build();
            }
            else {
                return ResponseEntity.badRequest().build();
            }



        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    public ResponseEntity registerWorker(NewWorkerData newWorkerData) {
        try {
            if (newWorkerData.getFirstName().equals("") || newWorkerData.getLastName().equals("") || newWorkerData.getEmail().equals("") || newWorkerData.getCategories().length == 0 || newWorkerData.getPassword().equals("")) {
                return ResponseEntity.badRequest().build();
            }
            Worker worker = new Worker(newWorkerData.getEmail(),passwordEncoder.encode(newWorkerData.getPassword()),newWorkerData.getFirstName(),newWorkerData.getLastName(),new UserRole(UserRoleEnum.WORKER),"");
            worker.setUserStatus(UserStatusEnum.ACCEPTED);
            for(String newCategory: newWorkerData.getCategories()) {
                Category category = categoryRepository.getTopByCategory(newCategory);
                if(category == null)
                    continue;

                worker.getCategories().add(category);
                category.getWorkers().add(worker);
                categoryRepository.save(category);
            }
            workerRepository.save(worker);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

    }

    public ResponseEntity getAllForAdmin(int page, int size){
        try {
            List<HashMap<String,Object>> usersList = new ArrayList<>();

            for(Worker worker : workerRepository.findAll()) {
                HashMap<String,Object> user = new HashMap<>();

                user.put("id",worker.getId());
                user.put("firstName",worker.getFirstName());
                user.put("lastName",worker.getLastName());
                user.put("email",worker.getEmail());
                user.put("phoneNumber",worker.getPhoneNumber());
                StringBuilder categories = new StringBuilder();
                for(Category category : worker.getCategories())
                    categories.append(category.getCategory()).append(" ");
                user.put("categories", categories.toString());
                user.put("status", UserStatusTranslation.translateRequestStatus(worker.getUserStatus()));
                usersList.add(user);

            }

            return ResponseEntity.ok().body(Page.createPage(page,size,usersList));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}