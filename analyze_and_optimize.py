#!/usr/bin/env python3
"""
Android Project Code Analyzer and Optimizer
Analyzes Java code for common issues and suggests optimizations
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Dict, Tuple

class CodeAnalyzer:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.issues = []
        self.stats = {
            'files_analyzed': 0,
            'issues_found': 0,
            'memory_leaks': 0,
            'performance_issues': 0,
            'code_quality': 0
        }
    
    def analyze_file(self, file_path: Path) -> List[Dict]:
        """Analyze a single Java file for issues"""
        issues = []
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                lines = content.split('\n')
            
            # Check for memory leaks
            issues.extend(self.check_memory_leaks(file_path, lines))
            
            # Check for performance issues
            issues.extend(self.check_performance_issues(file_path, lines))
            
            # Check for code quality
            issues.extend(self.check_code_quality(file_path, lines))
            
            self.stats['files_analyzed'] += 1
            self.stats['issues_found'] += len(issues)
            
        except Exception as e:
            print(f"Error analyzing {file_path}: {e}")
        
        return issues
    
    def check_memory_leaks(self, file_path: Path, lines: List[str]) -> List[Dict]:
        """Check for potential memory leaks"""
        issues = []
        
        # Static Activity/Context/View references
        for i, line in enumerate(lines):
            if re.search(r'static\s+.*\s*(Activity|Context|View|Fragment)\s+\w+', line):
                if 'WeakReference' not in line:
                    issues.append({
                        'file': str(file_path),
                        'line': i + 1,
                        'type': 'memory_leak',
                        'severity': 'high',
                        'message': 'Static reference to UI component can cause memory leak',
                        'suggestion': 'Use WeakReference for static UI references'
                    })
                    self.stats['memory_leaks'] += 1
        
        # Inner classes without static modifier
        class_pattern = re.compile(r'^\s*(private|public|protected)?\s*class\s+\w+')
        for i, line in enumerate(lines):
            if class_pattern.match(line) and 'static' not in line:
                # Check if it's an inner class
                if i > 0 and any(c in lines[i-1] for c in ['{', ';']):
                    issues.append({
                        'file': str(file_path),
                        'line': i + 1,
                        'type': 'memory_leak',
                        'severity': 'medium',
                        'message': 'Non-static inner class holds implicit reference to outer class',
                        'suggestion': 'Make inner class static if possible'
                    })
                    self.stats['memory_leaks'] += 1
        
        return issues
    
    def check_performance_issues(self, file_path: Path, lines: List[str]) -> List[Dict]:
        """Check for performance issues"""
        issues = []
        
        # String concatenation in loops
        in_loop = False
        loop_depth = 0
        for i, line in enumerate(lines):
            if re.search(r'\b(for|while)\s*\(', line):
                in_loop = True
                loop_depth += 1
            elif '}' in line and in_loop:
                loop_depth -= 1
                if loop_depth == 0:
                    in_loop = False
            
            if in_loop and '+' in line and '"' in line:
                if 'StringBuilder' not in line and 'StringBuffer' not in line:
                    issues.append({
                        'file': str(file_path),
                        'line': i + 1,
                        'type': 'performance',
                        'severity': 'medium',
                        'message': 'String concatenation in loop',
                        'suggestion': 'Use StringBuilder for string operations in loops'
                    })
                    self.stats['performance_issues'] += 1
        
        # findViewById calls in loops or frequently called methods
        for i, line in enumerate(lines):
            if 'findViewById' in line and in_loop:
                issues.append({
                    'file': str(file_path),
                    'line': i + 1,
                    'type': 'performance',
                    'severity': 'high',
                    'message': 'findViewById called in loop',
                    'suggestion': 'Cache view references outside loops'
                })
                self.stats['performance_issues'] += 1
        
        return issues
    
    def check_code_quality(self, file_path: Path, lines: List[str]) -> List[Dict]:
        """Check for code quality issues"""
        issues = []
        
        # Empty catch blocks
        for i, line in enumerate(lines):
            if re.search(r'catch\s*\([^)]+\)\s*\{', line):
                # Check if next non-empty line is closing brace
                j = i + 1
                while j < len(lines) and lines[j].strip() == '':
                    j += 1
                if j < len(lines) and lines[j].strip() == '}':
                    issues.append({
                        'file': str(file_path),
                        'line': i + 1,
                        'type': 'code_quality',
                        'severity': 'medium',
                        'message': 'Empty catch block',
                        'suggestion': 'Handle or log exceptions properly'
                    })
                    self.stats['code_quality'] += 1
        
        # Missing null checks
        for i, line in enumerate(lines):
            if '.getText()' in line or '.getString()' in line:
                if 'if' not in line and '?' not in line and '!=' not in line:
                    issues.append({
                        'file': str(file_path),
                        'line': i + 1,
                        'type': 'code_quality',
                        'severity': 'low',
                        'message': 'Potential null pointer exception',
                        'suggestion': 'Add null check before method call'
                    })
                    self.stats['code_quality'] += 1
        
        return issues
    
    def analyze_project(self):
        """Analyze all Java files in the project"""
        java_files = list(self.project_root.rglob('*.java'))
        
        print(f"Found {len(java_files)} Java files to analyze")
        
        for file_path in java_files:
            if 'build' not in str(file_path):  # Skip build directories
                file_issues = self.analyze_file(file_path)
                self.issues.extend(file_issues)
        
        return self.issues
    
    def generate_report(self):
        """Generate analysis report"""
        print("\n" + "="*60)
        print("CODE ANALYSIS REPORT")
        print("="*60)
        print(f"Files analyzed: {self.stats['files_analyzed']}")
        print(f"Total issues found: {self.stats['issues_found']}")
        print(f"Memory leak risks: {self.stats['memory_leaks']}")
        print(f"Performance issues: {self.stats['performance_issues']}")
        print(f"Code quality issues: {self.stats['code_quality']}")
        print("="*60)
        
        # Group issues by severity
        high_severity = [i for i in self.issues if i['severity'] == 'high']
        medium_severity = [i for i in self.issues if i['severity'] == 'medium']
        low_severity = [i for i in self.issues if i['severity'] == 'low']
        
        if high_severity:
            print(f"\nHIGH SEVERITY ISSUES ({len(high_severity)}):")
            for issue in high_severity[:10]:  # Show first 10
                print(f"  {issue['file']}:{issue['line']}")
                print(f"    {issue['message']}")
                print(f"    Suggestion: {issue['suggestion']}")
        
        if medium_severity:
            print(f"\nMEDIUM SEVERITY ISSUES ({len(medium_severity)}):")
            for issue in medium_severity[:5]:  # Show first 5
                print(f"  {issue['file']}:{issue['line']}")
                print(f"    {issue['message']}")
        
        print(f"\nLOW SEVERITY ISSUES: {len(low_severity)}")
        
        # Save detailed report
        with open(self.project_root / 'code_analysis_report.txt', 'w') as f:
            f.write("DETAILED CODE ANALYSIS REPORT\n")
            f.write("="*60 + "\n")
            for issue in self.issues:
                f.write(f"\n{issue['severity'].upper()}: {issue['type']}\n")
                f.write(f"File: {issue['file']}:{issue['line']}\n")
                f.write(f"Issue: {issue['message']}\n")
                f.write(f"Fix: {issue['suggestion']}\n")
        
        print(f"\nDetailed report saved to: code_analysis_report.txt")

def main():
    if len(sys.argv) > 1:
        project_root = sys.argv[1]
    else:
        project_root = '/workspace/app/src/main/java'
    
    analyzer = CodeAnalyzer(project_root)
    analyzer.analyze_project()
    analyzer.generate_report()

if __name__ == '__main__':
    main()