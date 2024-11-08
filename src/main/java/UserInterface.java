import manager.AccountManager;
import manager.BookManager;
import manager.MemoryAccountManager;
import manager.MemoryBookManager;
import models.*;
import record.*;

import java.util.List;
import java.util.Scanner;

public class UserInterface {
    private User user;
    private Scanner scanner;
    private BookManager bookManager = MemoryBookManager.getInstance();
    private AccountManager accountManager = MemoryAccountManager.getInstance();

    public UserInterface(User user) {
        this.user = user;
        this.scanner = new Scanner(System.in);
    }

    public void showMenu() {
        while (true) {
            System.out.println("--------------------------------------------------------------------------");
            System.out.println(" 사용자 메뉴");
            System.out.println("--------------------------------------------------------------------------");
            System.out.println("1. 도서 검색");
            System.out.println("2. 도서 대출");
            System.out.println("3. 도서 반납");
            System.out.println("4. 대출 현황 확인");
            System.out.println("5. 로그아웃");
            System.out.println("--------------------------------------------------------------------------");
            System.out.print("원하는 작업의 번호를 입력하세요: ");
            int choice = getUserChoice(1, 5);
            switch (choice) {
                case 1:
                    System.out.println("도서 검색 화면으로 이동합니다.");
                    handleSearchBook();
                    break;
                case 2:
                    System.out.println("도서 대출 화면으로 이동합니다.");
                    handleBorrowBook();
                    break;
                case 3:
                    System.out.println("도서 반납 화면으로 이동합니다.");
                    handleReturnBook();
                    break;
                case 4:
                    System.out.println("대출 현황 확인 화면으로 이동합니다.");
                    handleViewBorrowedBooks();
                    break;
                case 5:
                    System.out.println("로그아웃하고 초기화면으로 이동합니다.");
                    return;
                default:
                    break;
            }
        }
    }

    private int getUserChoice(int min, int max) {
        while (true) {
            if (scanner.hasNextInt()) {
                int choice = scanner.nextInt();
                scanner.nextLine();
                if (choice >= min && choice <= max) {
                    return choice;
                }
            } else {
                scanner.nextLine();
            }
            System.out.print("잘못된 입력입니다. " + min + "~" + max + " 사이의 번호로 다시 입력해주세요: ");
        }
    }

    private void handleSearchBook() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" 도서 검색 화면");
        System.out.println("--------------------------------------------------------------------------");


        String keyword;
        while (true) {
            System.out.print("검색할 키워드를 입력하세요 (제목 또는 저자): ");
            keyword = scanner.nextLine().trim();

            // 영어가 아닌 문자 포함 여부 검사
            if (keyword.isEmpty()) {
                System.out.println("검색어를 입력해주세요.");
            } else if (!keyword.matches("[a-zA-Z\\s\\d]+")) {
                System.out.println("잘못된 입력입니다. (영어 형태로 입력해주세요.)");
            } else {
                break;
            }

            if (!retryPrompt()) return;
        }

        List<Book> results = bookManager.searchBooks(keyword);
        if (results.isEmpty()) {
            System.out.println("입력하신 키워드에 해당하는 도서가 존재하지 않습니다. 사용자 메뉴 화면으로 이동합니다.");
        } else {
            System.out.println("--------------------------------------------------------------------------");
            System.out.println("검색 결과:");
            for (Book book : results) {
                System.out.println(book.getId() + ": " + book.getTitle() + " by " + book.getAuthor() +
                        (book.hasBorrowedCopies() ? " (대출 중)" : ""));
            }
            System.out.println("--------------------------------------------------------------------------");
        }
    }

    private void handleBorrowBook() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" 도서 대출 화면");
        System.out.println("--------------------------------------------------------------------------");

        if (user.hasOverdueBooks()) {
            System.out.println("연체된 미반납 도서가 있어 대출할 수 없습니다. 먼저 연체된 도서를 반납해주세요.");
            return;
        }

        while (true) {
            System.out.print("대출할 도서의 ID를 입력하세요: ");
            String inputId = scanner.nextLine();
            if (!isValidBookId(inputId)) {
                System.out.println("잘못된 입력입니다. (정수 형태로 입력해주세요.)");
                if (!retryPrompt()) return;
                continue;
            }

            int bookId = Integer.parseInt(inputId);
            Book book = bookManager.getBookById(bookId);
            if (book == null) {
                System.out.println("입력하신 ID에 해당하는 도서가 존재하지 않습니다. 사용자 메뉴 화면으로 이동합니다.");
                return;
            }

            if (user.hasBorrowedBook(book)) {
                System.out.println("입력하신 ID에 해당하는 도서를 이미 대출 했습니다. 사용자 메뉴 화면으로 이동합니다.");
                return;
            }

            if (book.getAvailableCopies() == 0) {
                System.out.println("대출 가능한 복사본이 없습니다. 사용자 메뉴 화면으로 이동합니다.");
                return;
            }

            BookCopy copy = book.borrowAvailableCopy();
            if (copy != null) {
                user.borrowBook(copy.getCopyId());
                BorrowRecord newBorrowRecord = new BorrowRecord(user.getId(), bookId, copy.getCopyId(),LastAccessRecord.getInstance().getLastAccessDate());

                //todo
                if (copy.getReturnDate() != null) {
                    newBorrowRecord.setReturnDate(copy.getReturnDate());
                }

                user.addBorrowRecord(newBorrowRecord);
                bookManager.saveData();
                accountManager.saveData();
                System.out.println("도서 대출이 성공적으로 완료되었습니다. 사용자 메뉴 화면으로 이동합니다.");
            } else {
                System.out.println("대출에 실패했습니다. 사용자 메뉴 화면으로 이동합니다.");
            }
            return;
        }
    }

    private void handleReturnBook() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" 도서 반납 화면");
        System.out.println("--------------------------------------------------------------------------");
        while (true) {
            System.out.print("반납할 도서의 ID를 입력하세요: ");
            String inputId = scanner.nextLine();
            if (!isValidBookId(inputId)) {
                System.out.println("잘못된 입력입니다. (정수 형태로 입력해주세요.)");
                if (!retryPrompt()) return;
                continue;
            }

            int bookId = Integer.parseInt(inputId);
            Book book = bookManager.getBookById(bookId);
            if (book == null) {
                System.out.println("입력하신 ID에 해당하는 도서가 존재하지 않습니다. 사용자 메뉴 화면으로 이동합니다.");
                return;
            }

            if (!user.hasBorrowedBook(book)) {
                System.out.println("해당 도서는 귀하가 대출한 도서가 아닙니다.");
                return;
            }

            user.returnBook(bookId);
            bookManager.saveData();
            ReturnRecord newReturnRecord = new ReturnRecord(user.getId(), bookId, LastAccessRecord.getInstance().getLastAccessDate());
            user.addReturnRecord(newReturnRecord);
            System.out.println("도서 반납이 성공적으로 완료되었습니다. 사용자 메뉴 화면으로 이동합니다.");
            return;
        }
    }

    private void handleViewBorrowedBooks() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" 대출 현황 확인 화면");
        System.out.println("--------------------------------------------------------------------------");
        while (true) {
            System.out.print("대출 현황을 확인할 도서의 ID를 입력하세요: ");
            String inputId = scanner.nextLine();
            if (!isValidBookId(inputId)) {
                System.out.println("잘못된 입력입니다. (정수 형태로 입력해주세요.)");
                if (!retryPrompt()) return;
                continue;
            }

            int bookId = Integer.parseInt(inputId);
            Book book = bookManager.getBookById(bookId);
            if (book == null) {
                System.out.println("입력하신 ID에 해당하는 도서가 존재하지 않습니다. 사용자 메뉴 화면으로 이동합니다.");
                return;
            }

            if (book.getAvailableCopies() > 0) {
                System.out.println("검색하신 도서는 대출이 가능합니다. 사용자 메뉴 화면으로 이동합니다.");
            } else {
                System.out.println("검색하신 도서는 이미 대출 중입니다. 사용자 메뉴 화면으로 이동합니다.");
            }
            return;
        }
    }

    private boolean isValidBookId(String id) {
        return id.matches("^(0|[1-9]\\d*)$");
    }

    private boolean retryPrompt() {
        System.out.print("다시 입력하시겠습니까? (y / 다른 키를 입력하면 사용자 메뉴 화면으로 이동합니다.): ");
        String retry = scanner.nextLine();
        return "y".equals(retry);
    }
}
