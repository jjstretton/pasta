/*
MIT License

Copyright (c) 2012-2017 PASTA Contributors (see CONTRIBUTORS.txt)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package pasta.domain.form;

import org.springframework.web.multipart.commons.CommonsMultipartFile;

/**
 * @author Joshua Stretton
 * @version 1.0
 * @since 30 Jun 2015
 */
public class UpdateUsersForm {
	private boolean updateTutors;
	private boolean replace;
	private String updateContents;
	private CommonsMultipartFile file;
	
	public UpdateUsersForm() {
		updateTutors = false;
		replace = false;
		updateContents = "";
		file = null;
	}

	public boolean isUpdateTutors() {
		return updateTutors;
	}

	public void setUpdateTutors(boolean updateTutors) {
		this.updateTutors = updateTutors;
	}
	
	public boolean isReplace() {
		return replace;
	}

	public void setReplace(boolean replace) {
		this.replace = replace;
	}

	public String getUpdateContents() {
		return updateContents;
	}

	public void setUpdateContents(String updateContents) {
		this.updateContents = updateContents;
	}

	public CommonsMultipartFile getFile() {
		return file;
	}

	public void setFile(CommonsMultipartFile file) {
		this.file = file;
	}
}
