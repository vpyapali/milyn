/*
	Milyn - Copyright (C) 2006

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License (version 2.1) as published by the Free Software
	Foundation.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

	See the GNU Lesser General Public License for more details:
	http://www.gnu.org/licenses/lgpl.txt
*/
package example;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.xml.transform.stream.StreamSource;

import org.milyn.Smooks;
import org.milyn.SmooksException;
import org.milyn.container.ExecutionContext;
import org.milyn.event.report.HtmlReportGenerator;
import org.milyn.io.StreamUtils;
import org.milyn.persistence.util.PersistenceUtil;
import org.milyn.routing.db.StatementExec;
import org.milyn.scribe.adapter.jpa.EntityManagerRegister;
import org.milyn.scribe.register.MapRegister;
import org.milyn.util.HsqlServer;
import org.xml.sax.SAXException;

import example.dao.CustomerDao;
import example.dao.OrderDao;
import example.dao.ProductDao;

/**
 * Simple example main class.
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class Main {

    private HsqlServer dbServer;

    private EntityManagerFactory emf;

    private EntityManager em;

    public static byte[] messageInDao = readInputMessage("dao");

    public static byte[] messageInJpa = readInputMessage("jpa");

    public static void main(String[] args) throws Exception {
        Main main = new Main();

        System.out.println("\n\nThis sample will use Smooks to extract data from an message and load it into a Database (Hypersonic)\n");


        try {
        	Main.pause("First the database needs be started. Press return to start the database...");

        	main.startDatabase();

        	main.initDatabase();

            System.out.println();

            Main.pause("The database is started now. Press return to see its contents.");

            main.printOrders();

            System.out.println();

            System.out.println("\n\nThis first run Smooks will use data access objects to persist and lookup entities.");

            Main.pause("Press return to see the sample message for the first run..");

            System.out.println("\n" + new String(messageInDao) + "\n");

            Main.pause("Now press return to execute Smooks.");

            main.runSmooksTransformWithDao();

            System.out.println();

            Main.pause("Smooks has processed the message.  Now press return to view the contents of the database again.  This time there should be orders and orderlines...");

            main.printOrders();

            System.out.println("\n\nThis second run Smooks will use JPA to persist and lookup entities.");

            Main.pause("Press return to see the sample message for the second run..");

            System.out.println("\n" + new String(messageInJpa) + "\n");
            System.out.println();

            Main.pause("Now press return to execute Smooks.");

            main.runSmooksTransformWithJpa();

            System.out.println();

            Main.pause("Smooks has processed the message.  Now press return to view the contents of the database again.  This time there should be new orders and orderlines...");

            main.printOrders();

            Main.pause("And that's it! Press return exit...");
        } finally {
            main.stopDatabase();
        }
    }

    protected void runSmooksTransformWithDao() throws IOException, SAXException, SmooksException {



    	Smooks smooks = new Smooks("./smooks-configs/smooks-dao-config.xml");
        ExecutionContext executionContext = smooks.createExecutionContext();

        // Configure the execution context to generate a report...
        executionContext.setEventListener(new HtmlReportGenerator("target/report/report-dao.html"));

        MapRegister<Object> mapRegister = new MapRegister<Object>();
        mapRegister.put("product", new ProductDao(em));
        mapRegister.put("customer", new CustomerDao(em));
        mapRegister.put("order", new OrderDao(em));

        PersistenceUtil.setDAORegister(executionContext, mapRegister);

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        smooks.filter(new StreamSource(new ByteArrayInputStream(messageInDao)), null, executionContext);


        tx.commit();

    }

    protected void runSmooksTransformWithJpa() throws IOException, SAXException, SmooksException {



    	Smooks smooks = new Smooks("./smooks-configs/smooks-jpa-config.xml");
        ExecutionContext executionContext = smooks.createExecutionContext();

        // Configure the execution context to generate a report...
        executionContext.setEventListener(new HtmlReportGenerator("target/report/report-jpa.html"));

        PersistenceUtil.setDAORegister(executionContext, new EntityManagerRegister(em));

        EntityTransaction tx = em.getTransaction();

        tx.begin();

        smooks.filter(new StreamSource(new ByteArrayInputStream(messageInJpa)), null, executionContext);

        tx.commit();

    }

    public void printOrders() throws SQLException {
    	List<Map<String, Object>> customers = getCustomers();
    	List<Map<String, Object>> products = getProducts();
        List<Map<String, Object>> orders = getOrders();
        List<Map<String, Object>> orderItems = getOrderItems();

        printResultSet("Customers", customers);
        printResultSet("Products", products);
        printResultSet("Orders", orders);
        printResultSet("Order Items", orderItems);
    }

    public List<Map<String, Object>> getOrders() throws SQLException {
    	StatementExec exec1OrderItems = new StatementExec("select * from orders");
        List<Map<String, Object>> rows = exec1OrderItems.executeUnjoinedQuery(dbServer.getConnection());
        return rows;
    }

    public List<Map<String, Object>> getOrderItems() throws SQLException {
        StatementExec exec1OrderItems = new StatementExec("select * from orderlines");
        List<Map<String, Object>> rows = exec1OrderItems.executeUnjoinedQuery(dbServer.getConnection());
        return rows;
    }

    public List<Map<String, Object>> getProducts() throws SQLException {
        StatementExec exec1OrderItems = new StatementExec("select * from products");
        List<Map<String, Object>> rows = exec1OrderItems.executeUnjoinedQuery(dbServer.getConnection());
        return rows;
    }

    public List<Map<String, Object>> getCustomers() throws SQLException {
        StatementExec exec1OrderItems = new StatementExec("select * from customers");
        List<Map<String, Object>> rows = exec1OrderItems.executeUnjoinedQuery(dbServer.getConnection());
        return rows;
    }

    private void printResultSet(String name, List<Map<String, Object>> resultSet) {
        System.out.println(("---- " + name + " -------------------------------------------------------------------------------------------------").substring(0, 80));
        if(resultSet.isEmpty()) {
            System.out.println("(No rows)");
        } else {
            for(int i = 0; i < resultSet.size(); i++) {
                Set<Map.Entry<String, Object>> row = resultSet.get(i).entrySet();

                System.out.println("Row " + i + ":");
                for (Map.Entry<String, Object> field : row) {
                    System.out.println("\t" + field.getKey() + ":\t" + field.getValue());
                }
            }
        }
        System.out.println(("---------------------------------------------------------------------------------------------------------------------").substring(0, 80));
    }

    public void startDatabase() throws Exception {
    	dbServer = new HsqlServer(9201);
        emf = Persistence.createEntityManagerFactory("db");
        em = emf.createEntityManager();
    }

    public void initDatabase() throws Exception {
    	InputStream schema = new FileInputStream("init-db.sql");

        try {
            dbServer.execScript(schema);
        } finally {
            schema.close();
        }
    }

    void stopDatabase() throws Exception {
    	try {
			em.close();
		} catch (Exception e) {
		}
    	try {
			emf.close();
		} catch (Exception e) {
		}
        dbServer.stop();
    }

    private static byte[] readInputMessage(String msg) {
        try {
            return StreamUtils.readStream(new FileInputStream("input-message-"+ msg +".xml"));
        } catch (IOException e) {
            e.printStackTrace();
            return "<no-message/>".getBytes();
        }
    }

    public void startEntityManagerFactory() throws Exception {


    	InputStream schema = new FileInputStream("db-create.script");

         try {
             dbServer = new HsqlServer(9201);
             dbServer.execScript(schema);
         } finally {
             schema.close();
         }

    }

    static void pause(String message) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("> " + message);
            in.readLine();
        } catch (IOException e) {
        }
        System.out.println("\n");
    }
}