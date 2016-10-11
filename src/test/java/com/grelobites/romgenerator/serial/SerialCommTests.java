package com.grelobites.romgenerator.serial;


import jssc.SerialPortList;
import org.junit.Test;

public class SerialCommTests {

    @Test
    public void getSystemComPorts() {
        String[] ports = SerialPortList.getPortNames();
        for (String port : ports) {
            System.out.println("Found port " + port);
        }
    }

}
