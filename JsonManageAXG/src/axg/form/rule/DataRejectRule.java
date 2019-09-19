/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.form.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author zz17390
 */
public class DataRejectRule {
    String newd;
    public String getNew(){
        return newd;
    }
    
    public void addNew(String nd){
        this.newd = nd.split("#")[0];
    }
    
    public String currentDate = "0";
    public void currentDATE(String date){
        currentDate = date;
    }
    
    public List<String> sbnList;
    public void setSBN(Set<String> sbns){
        sbnList = new ArrayList(sbns);
    }
}
