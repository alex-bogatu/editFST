package alexbogatu.github.editFST;


import org.junit.Test;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }
    
    /**
     * This is more of an integration test as it involves all functionalities.
     */
    @Test
    public void testApp()
    {
    	String str1 = "This is a sentence. It is made of words";
		String str2 = "This sentence is similar. It has almost the same words";
    	EditDistance edit = new EditDistance();
    	float editResult = edit.compare(str1, str2);
    	
        assertTrue(editResult > 0);
    }
}
