package io.yamil.bookshelf;

import java.util.ArrayList;
import java.util.List;

public class WrittenBook
{
    public String Title, Author;
    public List<String> Pages;
    public int totalPages;
    public int Generation;

    public WrittenBook(String Title, String Author, List<String> Pages, int totalPages)
    {
        this.Title = Title;
        this.Author = Author;
        if (Pages == null)
            Pages = new ArrayList<String>();
        this.Pages = Pages;
        this.totalPages = totalPages;

        Generation = 0;
    }


}
