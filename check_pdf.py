try:
    import pypdf
    print("pypdf available")
except ImportError:
    try:
        import PyPDF2
        print("PyPDF2 available")
    except ImportError:
        print("No PDF library found")
