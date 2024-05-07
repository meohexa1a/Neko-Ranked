package neko.testing.main;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MainApplication implements CommandLineRunner{
	@Autowired
    private DatacRepo datacRepo;

	@SuppressWarnings("unused")
	@Autowired
	private DatacController datacController;
	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}

	@Override
	public void run(String... args) {
		// Tạo một đối tượng Datac mới
		Random random = new Random();
		Datac datac = new Datac(random.nextInt(123456) + "", 123, "Hello MongoDB");

		// Lưu đối tượng vào MongoDB
		datacRepo.save(datac);

		// In ra thông tin đối tượng đã được lưu
		System.out.println("Saved into database");
		System.out.println("icid: " + datac.getIcid());
		System.out.println("storeInt: " + datac.getStoreInt());
		System.out.println("storeString: " + datac.getStoreString());

		System.exit(0);
	}
}
