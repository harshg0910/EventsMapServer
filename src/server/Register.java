package server;

import java.security.MessageDigest;
import javax.servlet.annotation.WebServlet;
import java.sql.*;
import java.util.Iterator;
import java.util.List;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Servlet implementation class Register
 */
@WebServlet("/Register")
public class Register extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	String user;
    String email;
    String password;
    String hash;
    String post;
    Connection conn;

    protected String hashFunc(String passwrd)
    		 throws Exception {
        String password = passwrd;

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(password.getBytes());

        byte byteData[] = md.digest();

        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < byteData.length; i++) {
         sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        System.out.println("Hex format : " + sb.toString());
        
        //convert the byte to hex format method 2
        StringBuffer hexString = new StringBuffer();
        for (int i=0;i<byteData.length;i++) {
                String hex=Integer.toHexString(0xff & byteData[i]);
                if(hex.length()==1) hexString.append('0');
                hexString.append(hex);
        }
        //System.out.println("Hex format : " + hexString.toString());
        return hexString.toString();
}

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    response.setContentType("text/html;charset=UTF-8");
    
    // get the return page for the current user
    String returnPage = Declarations.homePage(request);
    
    // If it's not administrator then, return
    if (returnPage != Declarations.adminHome) return;

    try{
        user = request.getParameter("user");
        email = request.getParameter("email");
        password = request.getParameter("pass");
        String repass = request.getParameter("repass");
        hash = hashFunc(password);
        post = request.getParameter("post");
        
        /*
    	 * Check the validity of the parameters obtained
    	 * */
    	String validityError = "";
    	if (user.isEmpty()) validityError += "<br /> User name is empty\n";
		if (email.isEmpty() || !Declarations.isValidEmail(email)) validityError += "<br /> Email is invalid \n";
		if (!password.equals(repass)) validityError += "<br /> Both passwords do not match \n";
		if (password.isEmpty()) validityError += "<br /> Empty password not allowed \n";
		if (post.isEmpty()) validityError += "<br /> Post cannot be empty \n";
    	
		List<String> posts = (List<String>) request.getSession(false).getAttribute("posts");
		Iterator<String> itr = posts.iterator();
		boolean found = false;
		while(itr.hasNext()){
			if (itr.next ().equalsIgnoreCase(post)) {
				found = true;
				validityError += "<br /> Post already in the  \n";
			}
		}
		if (!found) { 
			posts.add(post);
			request.getSession(false).setAttribute("posts",posts);
		}
		
		// check the validity error string for any errors
		if (!validityError.isEmpty()) {
			System.out.println (validityError);
    		request.setAttribute("error", validityError);
    		request.getRequestDispatcher(Declarations.registerHome).forward(request, response);
    		return;
		}
    }
    catch(Exception e){
    	System.out.println("Error. in register");
    	e.printStackTrace();
    	String error = "Error: Incomplete or Invalid Form Submitted";
		request.setAttribute("error", error);
    	request.getRequestDispatcher(Declarations.registerHome).forward(request, response);
    	return;
    }

    try{
	    String mysqlUser = Declarations.mysqlUser;
	    String mysqlPass = Declarations.mysqlPass;
	    String url = Declarations.url;
	    Class.forName("com.mysql.jdbc.Driver").newInstance();
	    conn = DriverManager.getConnection(url, mysqlUser, mysqlPass);
	    System.out.println("Checking your identity.."+ user +"\n");

	    if(conn!= null){
	    		 PreparedStatement s = null;
	             String query = "INSERT INTO Login (userName, passwdHash, email, post) VALUES (?,?,?,?)";
	             s = conn.prepareStatement(query);
	             s.setString(1, user);
	             s.setString(2, hash);
	             s.setString(3, email);
	             s.setString(4, post);
	             
	             System.out.println(query);
	             System.out.println(s.toString());
	             s.executeUpdate();
	             s.close();
	             
	             Declarations.closeConnection(conn);
	             //request.getRequestDispatcher(Declarations.registerHome).forward(request, response);
	             request.getRequestDispatcher(returnPage).forward(request, response);
	             return;
	    }
	    else{
	            System.out.println("Cannot connect to the database\n");
	            request.getRequestDispatcher(Declarations.registerHome).forward(request, response);
	            return;
	    }
    }
    catch (SQLException e) {
    	Declarations.closeConnection(conn);
    	System.out.println("Error : " + e.toString());
    	e.printStackTrace();
    	String error = e.getMessage();
    	System.out.println (e.getMessage());
		request.setAttribute("error", error);
        request.getRequestDispatcher(Declarations.registerHome).forward(request, response);
        return;
    }
    catch(Exception e){
    	Declarations.closeConnection(conn);
        System.out.println("Error : " + e.toString());
        request.getRequestDispatcher(Declarations.registerHome).forward(request, response);
    }
}

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    	/* If admin is not the current user, then return Home
    	 * */
    	String returnPage = Declarations.homePage(request);
    	if (returnPage == Declarations.adminHome)
    		processRequest(request, response);
    	else {
    		System.out.println ("Error: something malicious going on.");
    		request.getRequestDispatcher(returnPage).forward(request, response);
    	}
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		String returnPage = Declarations.homePage(request);
    	if (returnPage == Declarations.adminHome)
    		processRequest(request, response);
    	else{
    		System.out.println ("Error: something malicious going on.");
    		request.getRequestDispatcher(returnPage).forward(request, response);
    	}
	}
}
