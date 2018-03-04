package com.my.myapplication;

import java.util.HashMap;
import java.util.Map;

public class Sign {
	public static final Map<Integer, Integer> myMap = new HashMap<Integer, Integer>() {{
	    
	}};

	static{
		myMap.put(0, R.drawable.s0);
		myMap.put(1, R.drawable.s1);
		myMap.put(2, R.drawable.s2);
		myMap.put(3, R.drawable.s3);
		myMap.put(4, R.drawable.s4);
		myMap.put(5, R.drawable.s5);
		myMap.put(6, R.drawable.s6);
		myMap.put(7, R.drawable.s7);
		myMap.put(8, R.drawable.s8);
		myMap.put(9, R.drawable.s9);
		myMap.put(10, R.drawable.s10);
		myMap.put(11, R.drawable.s11);
//		myMap.put(12, R.drawable.s12);
//		myMap.put(13, R.drawable.s13);
//		myMap.put(14, R.drawable.s14);
//		myMap.put(15, R.drawable.s15);
//		myMap.put(16, R.drawable.s16);
//		myMap.put(17, R.drawable.s17);
//		myMap.put(18, R.drawable.s18);
//		myMap.put(19, R.drawable.s19);
//		myMap.put(20, R.drawable.s20);
//		myMap.put(21, R.drawable.s21);
//		myMap.put(22, R.drawable.s22);
//		myMap.put(23, R.drawable.s23);
//		myMap.put(24, R.drawable.s24);
//		myMap.put(25, R.drawable.s25);
	}
	private Integer id;
	private Float score;
	private Float runTime;
	
	public Sign(Integer id, Float score,Float runTime) {
		super();
		this.id = id;
		this.score = score;
		this.runTime=runTime;
	}

	public Sign() {

	}

	public Integer getImage() {
		return myMap.get(id);
	}

	public Integer getId() {
		return id;
	}

	public Float getScore() {
		return score;
	}

	public Float getRunTime() {
		return runTime;
	}

	public void setScore(Float score) {
		this.score = score;
	}

}
