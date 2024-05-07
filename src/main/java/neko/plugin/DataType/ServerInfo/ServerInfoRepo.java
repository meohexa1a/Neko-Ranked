package neko.plugin.DataType.ServerInfo;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ServerInfoRepo extends MongoRepository<ServerInfo, String> {
    List<ServerInfo> findByStatus(ServerStatus status);
}
