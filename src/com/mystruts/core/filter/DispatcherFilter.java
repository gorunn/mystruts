package com.mystruts.core.filter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import com.mystruts.core.element.ActionElement;
import com.mystruts.core.element.ResultElement;

public class DispatcherFilter extends AbstractFilter{

	//����һ��map���ϣ��洢������xmldom�ṹ
	private Map<String, ActionElement> xmlMap = new HashMap<String, ActionElement>();
	
	@SuppressWarnings("unchecked")
	public void init(FilterConfig filterConfig) 
			throws ServletException {

		SAXReader reader = new SAXReader();
		try {
			Document document = reader.read(DispatcherFilter.class.getClassLoader().getResourceAsStream("config.xml"));
			Element rootElement = document.getRootElement();
			List<Element> actions = rootElement.elements("action");
			for(Element action: actions){
				ActionElement actionElement = new ActionElement();
				System.out.println(action.getName());
				String actionName = action.attributeValue("name");
				String actionClass = action.attributeValue("class");
				String actionMethod = action.attributeValue("method");
				actionElement.setActionName(actionName);
				actionElement.setActionClass(actionClass);
				if(actionMethod != null){
					actionElement.setActionMethod(actionMethod);
				}
				List<ResultElement> resultElements = new ArrayList<ResultElement>();
				System.out.println("name: "+ actionElement.getActionName() +
						" class : "+ actionElement.getActionClass() + 
						" method : "+actionElement.getActionMethod());
				List<Element> results = action.elements("result");
				for(Element result : results){
					ResultElement resultElement = new ResultElement();
					resultElement.setResultName(result.attributeValue("name"));
					resultElement.setResultType(result.attributeValue("type"));
					resultElement.setViewName(result.getText());
					resultElements.add(resultElement);
					System.out.println(" name: "+ resultElement.getResultName() +
							" type: " + resultElement.getResultType() +
							" viewName: " +resultElement.getViewName());
				}
				actionElement.setResultElements(resultElements);
				xmlMap.put(actionName, actionElement);
			}
		} catch (DocumentException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	};
	
	
	@Override
	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		
		String requestURL = request.getRequestURI();
		System.out.println(requestURL);
		 //ֻ����.action��β������
		if(requestURL.endsWith(".action")){
			//��������·������ȡ�������� /home.action �� home 
			String requestName = requestURL.substring(1, requestURL.indexOf(".action"));
			System.out.println("requestName : " + requestName);
			//Ȼ��ʼ�Ƚ�xmlMap�Ƿ������requestName��key
			if(xmlMap.containsKey(requestName)){
				
				ActionElement actionElement = xmlMap.get(requestName);
				System.out.println("����ķ����ǣ� " + actionElement.getActionMethod());
				try {
				 	Class<?> clazz = Class.forName(actionElement.getActionClass());
					Object obj = clazz.newInstance();
					
					Enumeration<String> paramNames = request.getParameterNames();
					String paramName = null;
					String paramValue = null;
					while(paramNames.hasMoreElements()){
						paramName = paramNames.nextElement();
						paramValue = request.getParameter(paramName);
						////////////////////
						String getName = new StringBuffer("get")
							.append(paramName.substring(0,1).toUpperCase())
							.append(paramName.substring(1)).toString();
						Method getMethod = clazz.getMethod(getName);
					    System.out.println("getMethodClass: " + getMethod.getClass());
					    System.out.println("getMethodName: " + getMethod.getName());
					    System.out.println("getMethodReturnType: " + getMethod.getReturnType());
					    //��������ת��
						///////////////////////
						
						String setName = new StringBuffer("set")
									.append(paramName.substring(0,1).toUpperCase())
									.append(paramName.substring(1)).toString();
						Method setMethod = clazz.getMethod(setName, getMethod.getReturnType());
						//Object paramObj = getMethod.getReturnType().newInstance();
						String paramTypeName = getMethod.getReturnType().getName();
						System.out.println("getMethodName : " + paramTypeName);
						if(paramTypeName.equals("java.lang.Integer")){
							setMethod.invoke(obj, Integer.valueOf(paramValue));
						}else if(paramTypeName.equals("java.lang.Long")){
							setMethod.invoke(obj, Long.valueOf(paramValue));
						}else if(paramTypeName.equals("java.lang.Double")){
							setMethod.invoke(obj, Double.valueOf(paramValue));
						}else if(paramTypeName.equals("java.lang.Float")){
							setMethod.invoke(obj, Float.valueOf(paramValue));
						}else if(paramTypeName.equals("int")){
							setMethod.invoke(obj, Integer.parseInt(paramValue));
						}else if(paramTypeName.equals("long")){
							setMethod.invoke(obj, Long.parseLong(paramValue));
						}else if(paramTypeName.equals("double")){
							setMethod.invoke(obj, Double.parseDouble(paramValue));
						}else if(paramTypeName.equals("floata")){
							setMethod.invoke(obj, Float.parseFloat(paramValue));
						}else if(paramTypeName.equals("java.lang.String")){
							setMethod.invoke(obj, paramValue);
						}
						
					}
					
					Method method = clazz.getMethod(actionElement.getActionMethod());
					
					
					String resultString = (String) method.invoke(obj);
					
					ResultElement resultElement = null;
					for(ResultElement result : actionElement.getResultElements()){
						if(result.getResultName().equals(resultString)){
							resultElement = result;
							break;
						}
					}
					if(resultElement == null){
						response.sendError(404, "��ȷ��������result�ڵ��name������ô?");
					}else{
						if(resultElement.getResultType() == null){
							//����ת��
							request.getRequestDispatcher(resultElement.getViewName()).forward(request, response);
						}else if(resultElement.getResultType().equals("redirect")){
							response.sendRedirect(resultElement.getViewName());
						}else {
							response.sendError(404, "��ȷ�����������ʱת�������ض���ô?����ȷ����");
						}
					}
				
				} catch (Exception e) {
					System.out.println("�쳣�������쳣" + e.getMessage());
					e.printStackTrace();
				}
				
			}else{
				//���xmlMapû�а���requestName��key���򷵻�404
				response.sendError(404,"�������Դ�Ҳ���");
			}
		}else{
			System.out.println("doFilter");
			filterChain.doFilter(request, response);
		}
		
		
		
	}

}
