package com.challenge.telus.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Modelo para el mapeo de departamentos
 * Mapea nombres de departamentos a códigos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentMapping {
    
    private String departmentName;
    private String departmentCode;

}



