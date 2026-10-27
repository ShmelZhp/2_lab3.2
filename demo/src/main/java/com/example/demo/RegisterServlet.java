package com.example.demo;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String login = UserStore.normalize(req.getParameter("login"));
        String password = req.getParameter("password");
        String email = req.getParameter("email");

        String error = UserStore.get().register(login, password, email);
        if (error == null) {
            LoginServlet.signIn(req, login);              // сразу входим
            resp.sendRedirect(req.getContextPath() + "/files");
        } else {
            req.setAttribute("error", error);
            req.setAttribute("login", login);
            req.setAttribute("email", email);
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
        }
    }
}