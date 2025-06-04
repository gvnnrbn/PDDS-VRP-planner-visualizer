package pucp.pdds.backend.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class Message {
    private String  nickname;
    private String  content;
    private Date timestamp;
}
