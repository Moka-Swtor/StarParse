package com.ixale.starparse.domain;

import java.io.Serializable;
import java.util.Objects;

public class Entity implements Serializable {

	private static final long serialVersionUID = 1L;

	String name;
	Long guid;

	Entity() {
	}

	public Entity(String name, Long guid) {
		this.name = name;
		this.guid = guid;
	}

	public String getName() {
		return name;
	}

	public Long getGuid() {
		return guid;
	}

	public String toString() {
		return name + (guid != null ? " [" + guid + "]" : "");
	}

	public boolean attributeEquals(Entity entity) {
		if (this == entity) return true;
		if (entity == null) return false;
		return Objects.equals(name, entity.name) && Objects.equals(guid, entity.guid);
	}

}
