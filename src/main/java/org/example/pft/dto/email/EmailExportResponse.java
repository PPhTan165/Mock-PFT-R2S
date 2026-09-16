package org.example.pft.dto.email;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailExportResponse {

    private boolean success;

    private String message;

}
