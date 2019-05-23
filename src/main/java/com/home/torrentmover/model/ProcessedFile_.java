package com.home.torrentmover.model;

import java.util.Date;
import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2019-05-23T21:47:15.922+0100")
@StaticMetamodel(ProcessedFile.class)
public class ProcessedFile_ {
	public static volatile SingularAttribute<ProcessedFile, Integer> id;
	public static volatile SingularAttribute<ProcessedFile, String> fileName;
	public static volatile SingularAttribute<ProcessedFile, Date> dateProcessed;
	public static volatile SingularAttribute<ProcessedFile, FileType> fileType;
}
