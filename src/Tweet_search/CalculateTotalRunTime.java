/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;

/**
 *
 * @author ayan
 */
public class CalculateTotalRunTime {
    protected  long starttime;
    protected long endtime;
    
    public CalculateTotalRunTime(){
    this.starttime();
    } 
    protected final void starttime()
    {
      this.starttime=System.currentTimeMillis();
    }
    protected final void stoptime()
    {
      this.endtime=System.currentTimeMillis();
    }
    
    protected int get_Hours(long milisec)
    {
        return (int) (((milisec/1000)/60)/60);
    }
    protected int get_Minutes(long milisec)
    {
        return (int) (((milisec/1000)/60)%60);
    }
    protected int get_Seconds(long milisec)
    {
        return (int) ((milisec/1000)%60);
    }
    protected int getMiliSeconds(long milisec)
    {
        return (int) (milisec % 1000);
    }
    
    protected void PrintRunTime()
    {   
        this.stoptime();
        long difftime=this.endtime - this.starttime;
        
        System.err.println("Time taken: "+ get_Hours(difftime)+"hrs "+get_Minutes(difftime)+"mins "+get_Seconds(difftime)+"sec "+getMiliSeconds(difftime)+"mili sec.");
    }
    
}
