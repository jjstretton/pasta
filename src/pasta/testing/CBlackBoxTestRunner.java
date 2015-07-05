package pasta.testing;

import java.io.File;

import pasta.util.ProjectProperties;

/**
 * @author Joshua Stretton
 * @version 1.0
 * @since 26 Jun 2015
 */
public class CBlackBoxTestRunner extends BlackBoxTestRunner {

	private static String TEMPLATE_FILENAME = ProjectProperties.getInstance().getProjectLocation() + "build_templates" + File.separator + "black_box_c_template.xml";
	
	public CBlackBoxTestRunner() {
		super(new File(TEMPLATE_FILENAME));
	}
}