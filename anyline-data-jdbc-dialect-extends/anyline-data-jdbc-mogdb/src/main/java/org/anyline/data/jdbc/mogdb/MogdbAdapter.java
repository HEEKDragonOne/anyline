package org.anyline.data.jdbc.mogdb;

import org.anyline.data.jdbc.opengauss.OpenGaussAdapter;
import org.anyline.metadata.type.DatabaseType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository("anyline.data.jdbc.adapter.mogdb")
public class MogdbAdapter extends OpenGaussAdapter {
	
	public DatabaseType type(){
		return DatabaseType.MogDB;
	}
	@Value("${anyline.data.jdbc.delimiter.mogdb:}")
	private String delimiter;

	@Override
	public void afterPropertiesSet()  {
		setDelimiter(delimiter);
	}
} 
