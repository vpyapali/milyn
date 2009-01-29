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
package org.milyn.javabean.programatic;

import junit.framework.TestCase;
import org.milyn.Smooks;
import org.milyn.container.ExecutionContext;
import org.milyn.event.ExecutionEvent;
import org.milyn.event.ExecutionEventListener;
import org.milyn.event.types.ElementPresentEvent;
import org.milyn.event.types.ElementVisitEvent;
import org.milyn.javabean.Bean;
import org.milyn.javabean.Header;
import org.milyn.javabean.Order;
import org.milyn.javabean.OrderItem;
import org.milyn.payload.JavaResult;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Programmatic
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public class ProgrammaticConfigTest extends TestCase {

    public void test_01() {
        Smooks smooks = new Smooks();

        Bean orderBean = new Bean(Order.class, "order", "/order", smooks);

        Bean headerBean = new Bean(Header.class, "header", "/order", smooks)
                                    .bindTo("order", orderBean)
                                    .bindTo("customerNumber", "header/customer/@number")
                                    .bindTo("customerName", "header/customer")
                                    .bindTo("privatePerson", "header/privatePerson");

        orderBean.bindTo("header", headerBean);
        orderBean.bindTo("orderItems", orderBean.newBean(ArrayList.class, "/order")
                                     .bindTo(orderBean.newBean(OrderItem.class, "order-item")
                                        .bindTo("productId", "order-item/product")
                                        .bindTo("quantity", "order-item/quantity")
                                        .bindTo("price", "order-item/price")));
        orderBean.bindTo("orderItems", orderBean.newBean(OrderItem[].class, "/order")
                                     .bindTo(orderBean.newBean(OrderItem.class, "order-item")
                                        .bindTo("productId", "order-item/product")
                                        .bindTo("quantity", "order-item/quantity")
                                        .bindTo("price", "order-item/price")));

        JavaResult result = new JavaResult();
        smooks.filter(new StreamSource(getClass().getResourceAsStream("../order-01.xml")), result);

        Order order = (Order) result.getBean("order");
        int identity = System.identityHashCode(order);

        assertEquals("Order:" + identity + "[header[null, 123123, Joe, false, Order:" + identity + "]\n" +
                     "orderItems[[{productId: 111, quantity: 2, price: 8.9}, {productId: 222, quantity: 7, price: 5.2}]]\n" +
                     "norderItemsArray[[{productId: 111, quantity: 2, price: 8.9}, {productId: 222, quantity: 7, price: 5.2}]]]", order.toString());
    }

    public void test_02_arrays_programmatic() {
        Smooks smooks = new Smooks();

        Bean orderBean = new Bean(Order.class, "order", "order", smooks);
        Bean orderItemArray = new Bean(OrderItem[].class, "orderItemsArray", "order", smooks);
        Bean orderItem = new Bean(OrderItem.class, "orderItem", "order-item", smooks);

        orderItem.bindTo("productId", "order-item/product");
        orderItemArray.bindTo(orderItem);
        orderBean.bindTo("orderItems", orderItemArray);

        execSmooksArrays(smooks);
    }

    public void test_02_arrays_xml() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("xmlconfig_01.xml"));
        execSmooksArrays(smooks);
    }

    private void execSmooksArrays(Smooks smooks) {
        JavaResult result = new JavaResult();
        ExecutionContext execContext = smooks.createExecutionContext();

        //execContext.setEventListener(new ExecListener());
        smooks.filter(new StreamSource(getClass().getResourceAsStream("order-01.xml")), result, execContext);

        Order order = (Order) result.getBean("order");
        int identity = System.identityHashCode(order);

        assertEquals("Order:" + identity + "[header[null]\n" +
                     "orderItems[null]\n" +
                     "norderItemsArray[[{productId: 111, quantity: null, price: 0.0}, {productId: 222, quantity: null, price: 0.0}]]]", order.toString());
    }

    private class ExecListener implements ExecutionEventListener {
        public void onEvent(ExecutionEvent event) {
            if(event instanceof ElementPresentEvent || event instanceof ElementVisitEvent) {
                return;
            }

            System.out.println(event);
        }
    }
}
