package com.mystruts.test.action;

//������
public class HomeAction {

	//�û���
	private String username;
	//�û�����
	private Integer age;
	
	public String execute(){
		System.out.println("HomeAction execute...");
		return "success";
	}
	
	public String toSave(){
		System.out.println("HomeAction toSave...");
		return "success";
	}
	
	public String save(){
		System.out.println("HomeAction save");
		System.out.println("username: " + username);
		System.out.println("age: " + age);
		return "success";                    
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}
	
	
	
}
